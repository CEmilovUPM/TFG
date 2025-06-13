from http.client import HTTPResponse

from flask import Blueprint, request, Response, jsonify
import json

from urllib3 import BaseHTTPResponse

from app.backend_client import get_client

auth = Blueprint("auth",__name__)


@auth.route("/login", methods=['POST'])
def login():
    data = request.get_json()
    client = get_client()
    response = client.request("POST","/auth/login",data=data)

    return forward_token(response)


@auth.route("/register", methods=['POST'])
def register():
    data = request.get_json()
    client = get_client()
    response = client.request("POST", "/auth/register", data=data)

    return forward_token(response)

@auth.route("/refresh", methods=['GET'])
def refresh():
    token = request.cookies.get('refreshToken')
    client = get_client()
    #CHANGE THE REFRESH ENDPOINT  TO RETURN THE COOKIE AS WELL
    response = client.request("POST")

def forward_token(response: BaseHTTPResponse):
    resp_body_bytes = bytes(response.data)
    resp_body_str = resp_body_bytes.decode('utf-8')

    resp_body_dict = dict()
    try:
        # Parse JSON body into a Python dict
        resp_body_dict = json.loads(resp_body_str)
        resp_body_dict["info"].pop("accessToken", None)
        resp_body_dict["info"].pop("refreshToken", None)
    except json.JSONDecodeError:
        # Fall back if it's not JSON
        resp_body_dict["info"] = {"message": "Something went wrong"}

    flask_response = Response(
        response=json.dumps(resp_body_dict),
        status=response.status,
        mimetype='application/json'
    )

    set_cookies = response.headers.getlist("Set-Cookie")
    for cookie in set_cookies:
        flask_response.headers.add("Set-Cookie", cookie)

    return flask_response

