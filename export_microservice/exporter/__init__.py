import os

from flask import Flask



def create_app():
    BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../"))
    app = Flask(__name__)
    from exporter.export_view import export as export_blueprint
    app.register_blueprint(export_blueprint)
    return app
