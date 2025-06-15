from flask import Blueprint, render_template, request, redirect, json, make_response, jsonify
import re

from app.backend_client import get_client, RequestBuilder

protected = Blueprint("protected", __name__)


@protected.route("/profile", methods=["GET"])
def profile():
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return jsonify({'error': 'unauthorized'}), 401

    client = get_client()

    backend_request = RequestBuilder()
    backend_request.auth(token).refresh(refresh).set_method("get").set_endpoint("/user/profile")
    response = client.request_reauth(backend_request)

    if response.status in [401,403]:
        return jsonify({'error': 'unauthorized'}), 401

    body = json.loads(response.data)

    return body, 200


def target_profile(token, refresh, user_id):
    client = get_client()
    target_user = dict()
    profile_request = RequestBuilder()
    (profile_request
     .auth(token)
     .refresh(refresh)
     .set_method("get")
     .set_endpoint(f"/user/{user_id}/profile"))
    profile_response = client.request_reauth(profile_request)
    if profile_response.status == 200:
        body_profile = json.loads(profile_response.data)["data"][0]
        target_user["target_name"] = body_profile["name"]
        target_user["target_email"] = body_profile["email"]
    return target_user, profile_request.access_token


@protected.route("/user/<int:user_id>/goals",methods=["GET"])
def goals_list(user_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')
    client = get_client()

    user_data,_ =  profile()
    user_data = user_data["data"][0]
    token_user_id = user_data.pop("id", None)

    changed_token = token

    target_user = dict()
    if token_user_id != user_id:
        target_user, changed_token = target_profile(token,refresh, user_id)



    backend_request = ((((RequestBuilder()
                      .auth(changed_token))
                      .refresh(refresh))
                      .set_method("get"))
                      .set_endpoint(f"/user/{user_id}/goals"))
    response = client.request_reauth(backend_request)

    if response.status in [401,403]:
        return redirect('/')

    goals = json.loads(response.data)["data"]

    q = request.args.get('q', '').lower()

    if q:
        pattern = re.compile(re.escape(q), re.IGNORECASE)
        filtered_goals = [g for g in goals if pattern.search(g['title'])]
    else:
        filtered_goals = goals

    if token != backend_request.access_token:
        resp = make_response(render_template("goals.html",
                                             goals=filtered_goals,
                                             user_id=user_id,
                                             **user_data,
                                             **target_user))
        resp.set_cookie("JWT", backend_request.access_token)
        return resp

    return render_template("goals.html",
                           goals=filtered_goals,
                           user_id=user_id,
                           **user_data,
                           **target_user)



@protected.route("/api/user/<int:user_id>/goal/<int:goal_id>", methods=["DELETE"])
def goal_delete(user_id, goal_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')

    client = get_client()

    backend_request = ((((RequestBuilder()
                      .auth(token))
                      .refresh(refresh))
                      .set_method(request.method))
                      .set_endpoint(f"/user/{user_id}/goals/{goal_id}"))

    response = client.request_reauth(backend_request)

    if response.status in [401,403]:
        return redirect('/')

    if response.status == 204:
        return "", response.status
    return json.loads(response.data), response.status

@protected.route("/user/<int:user_id>/goals/<int:goal_id>", methods=["GET"])
def single_goal(user_id, goal_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')
    client = get_client()


    backend_request = ((((RequestBuilder()
                          .auth(token))
                         .refresh(refresh))
                        .set_method("get"))
                       .set_endpoint(f"/user/{user_id}/goals/{goal_id}"))
    response = client.request_reauth(backend_request)

    if response.status in [401,403]:
        return redirect('/')
    goal = json.loads(response.data)["data"][0]

    backend_request.set_endpoint(f"/user/{user_id}/goals/{goal_id}/progress")
    response = client.request_reauth(backend_request)
    progress_list = json.loads(response.data)["data"]

    q = request.args.get('q', '').lower()

    if q:
        pattern = re.compile(re.escape(q), re.IGNORECASE)
        filtered_progress = [p for p in progress_list if pattern.search(p['updateNote'])]
    else:
        filtered_progress = progress_list

    total_amount = 0.0
    for p in progress_list:
        total_amount += p["amount"]

    user_data, _ = profile()
    user_data = user_data["data"][0]
    token_user_id = user_data.pop("id", None)

    target_user = dict()
    if token_user_id != user_id:

        profile_request = RequestBuilder()
        (profile_request
         .auth(token)
         .refresh(refresh)
         .set_method("get")
         .set_endpoint(f"/user/{user_id}/profile"))
        profile_response = client.request_reauth(profile_request)
        if profile_response.status == 200:
            body_profile = json.loads(profile_response.data)["data"][0]
            target_user["target_name"] = body_profile["name"]
            target_user["target_email"] = body_profile["email"]


    if token != backend_request.access_token:
        resp = make_response(render_template("goal.html",
                                             goal=goal,
                                             progress_list=filtered_progress,
                                             progress_amount=total_amount,
                                             user_id=user_id,
                                             **user_data,
                                             **target_user))
        resp.set_cookie("JWT", backend_request.access_token)
        return resp

    return render_template("goal.html", goal=goal,
                           progress_list=filtered_progress,
                           progress_amount=total_amount,
                           user_id=user_id,
                           **user_data,
                           **target_user)

@protected.route("/user/<int:user_id>/goals/create-form", methods=["GET", "POST"])
def create_goal_form(user_id):
    if request.method == "POST":
        token = request.cookies.get('JWT')
        refresh = request.cookies.get('RefreshToken')
        if not refresh:
            return redirect('/')

        client = get_client()

        backend_request = ((((RequestBuilder()
                              .auth(token))
                             .refresh(refresh))
                            .set_method("post"))
                           .set_endpoint(f"/user/{user_id}/goals")
                           .set_json({
                               "title": request.form["title"],
                               "description": request.form.get("description", ""),
                               "totalDesiredAmount": float(request.form["totalDesiredAmount"]),
                               "metric": request.form["metric"]
                           }))

        response = client.request_reauth(backend_request)

        if response.status == 403 or response.status == 401:
            return redirect('/')

        if response.status == 201:
            return redirect(f'/user/{user_id}/goals')

        return render_template("goal_create_form.html",
                               error="Failed to create goal.",
                               user_id=user_id)

    return render_template("goal_create_form.html",
                           user_id=user_id)


@protected.route("/user/<int:user_id>/goals/<int:goal_id>/update-form", methods=["GET"])
def update_goal_form(user_id, goal_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')

    client = get_client()
    backend_request = ((((RequestBuilder()
                          .auth(token))
                         .refresh(refresh))
                        .set_method("get"))
                       .set_endpoint(f"/user/{user_id}/goals/{goal_id}"))
    response = client.request_reauth(backend_request)

    if response.status in [401, 403]:
        return redirect('/')

    goal = json.loads(response.data)["data"][0]
    return render_template("goal_update_form.html",
                           goal=goal,
                           user_id=user_id)


@protected.route("/user/<int:user_id>/goals/<int:goal_id>/update-form", methods=["POST"])
def update_goal(user_id, goal_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')

    form_data = {
        "title": request.form.get("title"),
        "description": request.form.get("description"),
        "totalDesiredAmount": float(request.form.get("totalDesiredAmount")),
        "metric": request.form.get("metric")
    }

    client = get_client()
    backend_request = ((((RequestBuilder()
                          .auth(token))
                         .refresh(refresh))
                        .set_method("patch")
                        .set_json(form_data))
                       .set_endpoint(f"/user/{user_id}/goals/{goal_id}"))
    response = client.request_reauth(backend_request)

    if response.status in [201, 200]:
        return redirect(f"/user/{user_id}/goals/{goal_id}")
    elif response.status in [401, 403]:
        return redirect('/')

    return render_template("goal_update_form.html",
                                            goal=form_data,
                                            error="Failed to update goal",
                                            user_id=user_id)

@protected.route("/user/<int:user_id>/goals/<int:goal_id>/progress-form", methods=["GET", "POST"])
def create_progress(user_id, goal_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')
    client = get_client()

    backend_request = ((((RequestBuilder()
                          .auth(token))
                         .refresh(refresh))
                        .set_method("get"))
                       .set_endpoint(f"/user/{user_id}/goals/{goal_id}"))
    response = client.request_reauth(backend_request)
    if response.status != 200:
        return redirect(f"/user/{user_id}/goals")

    goal = json.loads(response.data)["data"][0]
    goal_title = goal["title"]

    if request.method == "POST":
        form_data = {
            "amount": request.form["amount"],
            "updateNote": request.form["updateNote"]
        }

        backend_request = ((((RequestBuilder()
                              .auth(token))
                             .refresh(refresh))
                            .set_method("post"))
                           .set_endpoint(f"/user/{user_id}/goals/{goal_id}/progress")
                           .set_json(form_data))

        post_response = client.request_reauth(backend_request)

        body = json.loads(post_response.data)

        if post_response.status == 201:
            return redirect(f"/user/{user_id}/goals/{goal_id}")
        else:
            return render_template("progress_create_form.html",
                                   goal_id=goal_id,
                                   goal_title=goal_title,
                                   user_id=user_id,
                                   **body["errors"])

    return render_template("progress_create_form.html",
                           goal_id=goal_id,
                           user_id=user_id,
                           goal_title=goal_title)

@protected.route("/api/user/<int:user_id>/goal/<int:goal_id>/progress/<int:progress_id>", methods=["DELETE"])
def delete_progress(user_id, goal_id, progress_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')

    client = get_client()

    backend_request = ((((RequestBuilder()
                      .auth(token))
                      .refresh(refresh))
                      .set_method("DELETE"))
                      .set_endpoint(f"/user/{user_id}/goals/{goal_id}/progress/{progress_id}"))

    response = client.request_reauth(backend_request)

    if response.status in [403,401]:
        return redirect('/')

    if response.status == 204:
        return "", response.status
    return json.loads(response.data), response.status


@protected.route("/user/<int:user_id>/goals/<int:goal_id>/progress/<int:progress_id>/update-form", methods=["GET"])
def update_progress_form(user_id, goal_id, progress_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')

    client = get_client()
    backend_request = (
        RequestBuilder()
        .auth(token)
        .refresh(refresh)
        .set_method("get")
        .set_endpoint(f"/user/{user_id}/goals/{goal_id}/progress/{progress_id}")
    )

    response = client.request_reauth(backend_request)

    if response.status in [401, 403]:
        return redirect('/')

    progress = json.loads(response.data).get("data", [{}])[0]

    return render_template("progress_update_form.html",
                           progress=progress,
                           goal_id=goal_id,
                           user_id=user_id)


@protected.route("/user/<int:user_id>/goals/<int:goal_id>/progress/<int:progress_id>/update-form", methods=["POST"])
def update_progress(user_id, goal_id, progress_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')

    form_data = {
        "updateNote": request.form.get("updateNote"),
        "amount": request.form.get("amount"),
    }

    client = get_client()
    backend_request = (
        RequestBuilder()
        .auth(token)
        .refresh(refresh)
        .set_method("patch")
        .set_json(form_data)
        .set_endpoint(f"/user/{user_id}/goals/{goal_id}/progress/{progress_id}")
    )

    response = client.request_reauth(backend_request)

    if response.status in [200, 201]:
        return redirect(f"/user/{user_id}/goals/{goal_id}")
    elif response.status in [401, 403]:
        return redirect('/')

    get_request = (
        RequestBuilder()
        .auth(token)
        .refresh(refresh)
        .set_method("get")
        .set_endpoint(f"/user/{user_id}/goals/{goal_id}/progress/{progress_id}")
    )
    get_response = client.request_reauth(get_request)
    progress = json.loads(get_response.data).get("data", [{}])[0]

    body = json.loads(response.data)

    return render_template("progress_update_form.html",
                           progress=progress,
                           goal_id=goal_id,
                           user_id=user_id,
                           **body["errors"])

@protected.route("/admin", methods=["GET"])
def admin_dashboard():
    token = request.cookies.get("JWT")
    refresh = request.cookies.get("RefreshToken")
    if not refresh:
        return redirect("/")

    client = get_client()

    user_data, _ = profile()
    user_data = user_data["data"][0]

    if not user_data.get("isAdmin", False):
        return redirect(f"/user/{user_data['id']}/goals")

    backend_request = (
        RequestBuilder()
        .auth(token)
        .refresh(refresh)
        .set_method("get")
        .set_endpoint("/user")
    )
    response = client.request_reauth(backend_request)

    if response.status != 200:
        return redirect("/")

    q = request.args.get('q', '').lower()

    users = json.loads(response.data)["data"]

    if q:
        pattern = re.compile(re.escape(q), re.IGNORECASE)
        filtered_users = [u for u in users if pattern.search(u['name']) or pattern.search(u['email'])  ]
    else:
        filtered_users = users

    return render_template("admin_dashboard.html",
                           users=filtered_users,
                           **user_data)

@protected.route("/api/admin/user/<string:user_email>/<string:action>", methods=["POST"])
def admin_action(user_email, action):
    token = request.cookies.get("JWT")
    refresh = request.cookies.get("RefreshToken")
    if not refresh:
        return redirect("/")

    client = get_client()

    user_data, _ = profile()
    user_data = user_data["data"][0]

    if not user_data.get("isAdmin", False):
        return redirect(f"/user/{user_data['id']}/goals")

    backend_request = (
        RequestBuilder()
        .auth(token)
        .refresh(refresh)
        .set_method("post")
        .set_endpoint(f"/user/{action}")
        .set_json({"targetUser":user_email})
    )
    response = client.request_reauth(backend_request)

    if response.status in [401,403,404]:
        return None, response.status

    return json.loads(response.data), response.status


def trim_float(value):
    try:
        f = float(value)
        return int(f) if f.is_integer() else round(f,2)
    except (ValueError, TypeError):
        return value