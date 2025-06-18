import json

from flask import Response
from urllib3 import BaseHTTPResponse

import app.config as config
import urllib3


_STATIC_INSTANCE = None

def get_client():
    global _STATIC_INSTANCE
    if _STATIC_INSTANCE is None:
        _STATIC_INSTANCE = Backend(config.BACKEND_URL)
    return _STATIC_INSTANCE

class Token:
    def __init__(self, token):
        self._token = token

    @property
    def value(self):
        return self._token

    @value.setter
    def value(self, value):
        self._token = value


class RequestBuilder:
    def __init__(self):
        self._headers: dict | None = None
        self._method: str = 'get'
        self._endpoint: str = ''
        self._data: dict | None = None
        self._accessToken: Token | None = None
        self._refreshToken: Token | None = None

    @property
    def headers(self) -> dict | None:
        if self._headers is None:
            self._headers = {}
        self._headers['Authorization'] = f'Bearer {self._accessToken.value}'
        self._headers['RefreshToken'] = self._refreshToken.value
        return self._headers

    def set_headers(self, headers: dict):
        self._headers = headers
        return self

    def auth(self, token: Token):
        self._accessToken = token
        return self

    def refresh(self, token: Token):
        self._refreshToken = token
        return self

    @property
    def method(self) -> str:
        return self._method

    def set_method(self, method: str):
        self._method = method.lower()
        return self

    @property
    def endpoint(self) -> str:
        return self._endpoint

    def set_endpoint(self, endpoint: str):
        self._endpoint = endpoint
        return self

    @property
    def data(self) -> dict | None:
        return self._data

    def set_json(self, data: dict):
        self._data = data
        return self

    @property
    def access_token(self):
        return self._accessToken




class Backend:
    def __init__(self, url: str):
        self._url = url

    def request(self, method:str, endpoint:str, data:str|dict=None, headers=None)-> BaseHTTPResponse:
        url = f"{self._url.rstrip('/')}/{endpoint.lstrip('/')}"
        response = urllib3.request(method,url,json=data, headers=headers)
        print(f"\"{method.upper()} {url} HTTP/1.1\" {response.status}")
        return response

    def request_reauth(self, request: RequestBuilder)->BaseHTTPResponse:
        response = self.request(request.method, request.endpoint, data=request.data, headers=request.headers)
        if response.status == 401 or response.status == 403:
            new_auth = self.request( "post",
                                    "/auth/refresh",
                                            data={"refreshToken":f"{request.headers.get('RefreshToken')}"})
            try:
                body = json.loads(bytes(new_auth.data).decode('utf-8'))
                request.access_token.value = body["info"]["accessToken"]
                response = self.request(request.method, request.endpoint, data=request.data, headers=request.headers)
            except Exception as e:
                return response
        return response

