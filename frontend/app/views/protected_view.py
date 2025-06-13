from flask import Blueprint, render_template, request, redirect, json, make_response
import re

from app.backend_client import get_client, RequestBuilder

protected = Blueprint("protected", __name__)

@protected.route("/goals",methods=["GET"])
def goals_list():
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')

    client = get_client()
    backend_request = ((((RequestBuilder()
                      .auth(token))
                      .refresh(refresh))
                      .set_method("get"))
                      .set_endpoint("/goals"))
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
        resp = make_response(render_template("goals.html", goals=filtered_goals))
        resp.set_cookie("JWT", backend_request.access_token)
        return resp

    return render_template("goals.html",
                           goals=filtered_goals)



@protected.route("/api/goal/<int:goal_id>", methods=["DELETE"])
def goal_action(goal_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')

    client = get_client()

    backend_request = ((((RequestBuilder()
                      .auth(token))
                      .refresh(refresh))
                      .set_method(request.method))
                      .set_endpoint(f"/goals/{goal_id}"))

    response = client.request_reauth(backend_request)

    if response.status in [401,403]:
        return redirect('/')

    if response.status == 204:
        return "", response.status
    return json.loads(response.data), response.status

@protected.route("/goals/<int:goal_id>", methods=["GET"])
def single_goal(goal_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')
    client = get_client()


    backend_request = ((((RequestBuilder()
                          .auth(token))
                         .refresh(refresh))
                        .set_method("get"))
                       .set_endpoint(f"/goals/{goal_id}"))
    response = client.request_reauth(backend_request)

    if response.status in [401,403]:
        return redirect('/')

    goal = json.loads(response.data)["data"][0]

    backend_request.set_endpoint(f"/goals/{goal_id}/progress")

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


    if token != backend_request.access_token:
        resp = make_response(render_template("goal.html", goal=goal,
                                             progress_list=filtered_progress,
                                             progress_amount=total_amount))
        resp.set_cookie("JWT", backend_request.access_token)
        return resp

    return render_template("goal.html", goal=goal,
                           progress_list=filtered_progress,
                           progress_amount=total_amount)

@protected.route("/goals/create-form", methods=["GET", "POST"])
def create_goal_form():
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
                           .set_endpoint("/goals")
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
            return redirect('/goals')

        return render_template("goal_create_form.html", error="Failed to create goal.")

    return render_template("goal_create_form.html")


@protected.route("/goals/<int:goal_id>/update-form")
def update_goal_form(goal_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')

    client = get_client()
    backend_request = ((((RequestBuilder()
                          .auth(token))
                         .refresh(refresh))
                        .set_method("get"))
                       .set_endpoint(f"/goals/{goal_id}"))
    response = client.request_reauth(backend_request)

    if response.status in [401, 403]:
        return redirect('/')

    goal = json.loads(response.data)["data"][0]
    return render_template("goal_update_form.html", goal=goal)


@protected.route("/goals/<int:goal_id>/update-form", methods=["POST"])
def update_goal(goal_id):
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
                       .set_endpoint(f"/goals/{goal_id}"))
    response = client.request_reauth(backend_request)

    if response.status in [201, 200]:
        return redirect(f"/goals/{goal_id}")
    elif response.status in [401, 403]:
        return redirect('/')

    return render_template("goal_update_form.html",
                                            goal=form_data,
                                            error="Failed to update goal")

@protected.route("/goals/<int:goal_id>/progress-form", methods=["GET", "POST"])
def create_progress(goal_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')
    client = get_client()

    backend_request = ((((RequestBuilder()
                          .auth(token))
                         .refresh(refresh))
                        .set_method("get"))
                       .set_endpoint(f"/goals/{goal_id}"))
    response = client.request_reauth(backend_request)
    if response.status != 200:
        return redirect(f"/goals")

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
                           .set_endpoint(f"/goals/{goal_id}/progress")
                           .set_json(form_data))

        post_response = client.request_reauth(backend_request)

        body = json.loads(post_response.data)

        if post_response.status == 201:
            return redirect(f"/goals/{goal_id}")
        else:
            return render_template("progress_create_form.html",
                                   goal_id=goal_id,
                                   goal_title=goal_title,
                                   **body["errors"])

    return render_template("progress_create_form.html",
                           goal_id=goal_id,
                           goal_title=goal_title)

@protected.route("/api/goal/<int:goal_id>/progress/<int:progress_id>", methods=["DELETE"])
def delete_progress(goal_id, progress_id):
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return redirect('/')

    client = get_client()

    backend_request = ((((RequestBuilder()
                      .auth(token))
                      .refresh(refresh))
                      .set_method("DELETE"))
                      .set_endpoint(f"/goals/{goal_id}/progress/{progress_id}"))

    response = client.request_reauth(backend_request)

    if response.status in [403,401]:
        return redirect('/')

    if response.status == 204:
        return "", response.status
    return json.loads(response.data), response.status


@protected.route("/goals/<int:goal_id>/progress/<int:progress_id>/update-form", methods=["GET"])
def update_progress_form(goal_id, progress_id):
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
        .set_endpoint(f"/goals/{goal_id}/progress/{progress_id}")
    )

    response = client.request_reauth(backend_request)

    if response.status in [401, 403]:
        return redirect('/')

    progress = json.loads(response.data).get("data", [{}])[0]

    return render_template("progress_update_form.html", progress=progress, goal_id=goal_id)


@protected.route("/goals/<int:goal_id>/progress/<int:progress_id>/update-form", methods=["POST"])
def update_progress(goal_id, progress_id):
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
        .set_endpoint(f"/goals/{goal_id}/progress/{progress_id}")
    )

    response = client.request_reauth(backend_request)

    if response.status in [200, 201]:
        return redirect(f"/goals/{goal_id}")
    elif response.status in [401, 403]:
        return redirect('/')

    get_request = (
        RequestBuilder()
        .auth(token)
        .refresh(refresh)
        .set_method("get")
        .set_endpoint(f"/goals/{goal_id}/progress/{progress_id}")
    )
    get_response = client.request_reauth(get_request)
    progress = json.loads(get_response.data).get("data", [{}])[0]

    body = json.loads(response.data)

    return render_template("progress_update_form.html",
                           progress=progress,
                           goal_id=goal_id,
                           **body["errors"])

def trim_float(value):
    try:
        f = float(value)
        return int(f) if f.is_integer() else f
    except (ValueError, TypeError):
        return value