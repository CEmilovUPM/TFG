from flask import Blueprint, render_template, request, redirect, json, make_response, jsonify
import re

from app.backend_client import get_client, RequestBuilder, Token
from app.views.utils import filter_goals_list, filter_single_goal

protected = Blueprint("protected", __name__)


@protected.route("/profile", methods=["GET"])
def profile():
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
        return jsonify({'error': 'unauthorized'}), 401

    client = get_client()

    backend_request = RequestBuilder()
    backend_request.auth(token).refresh(refresh).set_method("get").set_endpoint("/user/profile")
    response_b = client.request_reauth(backend_request)

    if response_b .status in [401,403]:
        return jsonify({'error': 'unauthorized'}), 401

    body = json.loads(response_b.data)

    return body, 200


def target_profile(token:Token, refresh:Token, user_id):
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
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
        return redirect('/')
    client = get_client()

    user_data,status =  profile()
    if status in [401, 403]:
        return redirect('/')
    user_data = user_data["data"][0]
    token_user_id = user_data.pop("id", None)

    target_user = dict()
    if token_user_id != user_id:
        target_user, token = target_profile(token,refresh, user_id)


    backend_request = ((((RequestBuilder()
                      .auth(token))
                      .refresh(refresh))
                      .set_method("get"))
                      .set_endpoint(f"/user/{user_id}/goals"))
    response = client.request_reauth(backend_request)

    if response.status in [401,403]:
        return redirect('/')

    goals = json.loads(response.data)["data"]
    filtered_goals = filter_goals_list(request.args, goals)

    resp = make_response(render_template("goals.html",
                                             goals=filtered_goals,
                                             user_id=user_id,
                                             **user_data,
                                             **target_user))
    return create_response(resp, token)


@protected.route("/api/user/<int:user_id>/goal/<int:goal_id>", methods=["DELETE"])
def goal_delete(user_id, goal_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
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
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
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

    filtered_progress = filter_single_goal(request.args, progress_list)

    total_amount = 0.0
    for p in progress_list:
        total_amount += p["amount"]

    user_data, status = profile()
    if status in [401, 403]:
        return redirect('/')
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


    resp = make_response(render_template("goal.html",
                                         goal=goal,
                                         progress_list=filtered_progress,
                                         progress_amount=total_amount,
                                         user_id=user_id,
                                         **user_data,
                                         **target_user))

    return create_response(resp, token)

@protected.route("/user/<int:user_id>/goals/create-form", methods=["GET", "POST"])
def create_goal_form(user_id):
    if request.method == "POST":
        token = Token(request.cookies.get('JWT'))
        refresh = Token(request.cookies.get('RefreshToken'))
        if not refresh or not refresh.value:
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

        errors = json.loads(response.data)["errors"]

        resp = render_template("goal_create_form.html",
                               user_id=user_id,
                               **errors)

        return create_response(resp,token)

    return render_template("goal_create_form.html",
                           user_id=user_id)


@protected.route("/user/<int:user_id>/goals/<int:goal_id>/update-form", methods=["GET"])
def update_goal_form(user_id, goal_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
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

    resp = render_template("goal_update_form.html",
                           goal=goal,
                           user_id=user_id)
    return create_response(resp, token)


@protected.route("/user/<int:user_id>/goals/<int:goal_id>/update-form", methods=["POST"])
def update_goal(user_id, goal_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
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

    errors = json.loads(response.data)["errors"]

    resp = render_template("goal_update_form.html",
                                            goal=form_data,
                                            **errors,
                                            user_id=user_id)

    return create_response(resp, token)

@protected.route("/user/<int:user_id>/goals/<int:goal_id>/progress-form", methods=["GET", "POST"])
def create_progress(user_id, goal_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
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
            "amount": float(request.form["amount"]),
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
            resp = render_template("progress_create_form.html",
                                   goal_id=goal_id,
                                   goal_title=goal_title,
                                   user_id=user_id,
                                   **body["errors"])
            return create_response(resp, token)

    resp = render_template("progress_create_form.html",
                           goal_id=goal_id,
                           user_id=user_id,
                           goal_title=goal_title)
    return create_response(resp, token)

@protected.route("/api/user/<int:user_id>/goal/<int:goal_id>/progress/<int:progress_id>", methods=["DELETE"])
def delete_progress(user_id, goal_id, progress_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
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
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
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

    resp = render_template("progress_update_form.html",
                           progress=progress,
                           goal_id=goal_id,
                           user_id=user_id)

    return create_response(resp, token)


@protected.route("/user/<int:user_id>/goals/<int:goal_id>/progress/<int:progress_id>/update-form", methods=["POST"])
def update_progress(user_id, goal_id, progress_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
        return redirect('/')

    form_data = {
        "updateNote": request.form.get("updateNote"),
        "amount": float(request.form.get("amount")),
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

    resp = render_template("progress_update_form.html",
                           progress=progress,
                           goal_id=goal_id,
                           user_id=user_id,
                           **body["errors"])

    return create_response(resp, token)

@protected.route("/admin", methods=["GET"])
def admin_dashboard():
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
        return redirect("/")

    client = get_client()

    user_data, status = profile()
    if status in [401, 403]:
        return redirect('/')
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

    resp = render_template("admin_dashboard.html",
                           users=filtered_users,
                           **user_data)
    return create_response(resp, token)

@protected.route("/api/admin/user/<string:user_email>/<string:action>", methods=["POST"])
def admin_action(user_email, action):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
        return redirect("/")

    client = get_client()

    user_data, status = profile()
    if status in [401, 403]:
        return redirect('/')
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

def create_response(content, access_token: Token):
    out = make_response(content)
    out.set_cookie("JWT",access_token.value)
    return out
