from flask import request, redirect, json, make_response, render_template

from app.backend_client import Token, get_client, RequestBuilder
from app.views import protected
from app.views.protected_view import profile, target_profile
from app.views.utils import filter_goals_list, filter_single_goal, create_response


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


@protected.route("/api/user/<int:user_id>/goals/<int:goal_id>", methods=["DELETE"])
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
    user_data, status = profile()
    if status in [401, 403]:
        return redirect('/')
    user_data = user_data["data"][0]

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
                               **errors,
                               **user_data)

        return create_response(resp,token)

    return render_template("goal_create_form.html",
                           user_id=user_id,
                           **user_data)


@protected.route("/user/<int:user_id>/goals/<int:goal_id>/update-form", methods=["GET"])
def update_goal_form(user_id, goal_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
        return redirect('/')

    user_data, status = profile()
    if status in [401, 403]:
        return redirect('/')
    user_data = user_data["data"][0]

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
                           user_id=user_id,
                           **user_data,
                           goal_id=goal_id)
    return create_response(resp, token)


@protected.route("/user/<int:user_id>/goals/<int:goal_id>/update-form", methods=["POST"])
def update_goal(user_id, goal_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
        return redirect('/')

    user_data, status = profile()
    if status in [401, 403]:
        return redirect('/')
    user_data = user_data["data"][0]

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
                                            **user_data,
                                            user_id=user_id,
                                            goal_id=goal_id)

    return create_response(resp, token)
