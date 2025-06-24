from flask import render_template, request, redirect, json, jsonify
import re

from app.backend_client import get_client, RequestBuilder, Token
from app.views import protected
from app.views.utils import create_response


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


