from flask import request, redirect, json, render_template

from app.backend_client import Token, get_client, RequestBuilder
from app.views import protected
from app.views.protected_view import profile
from app.views.utils import create_response


@protected.route("/user/<int:user_id>/goals/<int:goal_id>/progress-form", methods=["GET", "POST"])
def create_progress(user_id, goal_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
        return redirect('/')
    client = get_client()

    user_data, status = profile()
    if status in [401, 403]:
        return redirect('/')
    user_data = user_data["data"][0]

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
                                   **body["errors"],
                                   **user_data)
            return create_response(resp, token)

    resp = render_template("progress_create_form.html",
                           goal_id=goal_id,
                           user_id=user_id,
                           goal_title=goal_title,
                           **user_data)
    return create_response(resp, token)


@protected.route("/api/user/<int:user_id>/goals/<int:goal_id>/progress/<int:progress_id>", methods=["DELETE"])
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

    user_data, status = profile()
    if status in [401, 403]:
        return redirect('/')
    user_data = user_data["data"][0]

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
                           user_id=user_id,
                           **user_data)

    return create_response(resp, token)


@protected.route("/user/<int:user_id>/goals/<int:goal_id>/progress/<int:progress_id>/update-form", methods=["POST"])
def update_progress(user_id, goal_id, progress_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh or not refresh.value:
        return redirect('/')

    user_data, status = profile()
    if status in [401, 403]:
        return redirect('/')
    user_data = user_data["data"][0]

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
                           **body["errors"],
                           **user_data)

    return create_response(resp, token)
