import os

from flask import Flask


def create_app():
    BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../"))
    app = Flask(__name__,
                template_folder=os.path.join(BASE_DIR, "templates"),
                static_folder=os.path.join(BASE_DIR, "static"))
    from graph.graph_view import protected as protected_blueprint

    app.register_blueprint(protected_blueprint)


    return app