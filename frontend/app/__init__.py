import os

from flask import Flask

from app.views.utils import render_date, trim_float


def create_app():
    BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../"))
    app = Flask(__name__,
                template_folder=os.path.join(BASE_DIR, "templates"),
                static_folder=os.path.join(BASE_DIR, "static"))

    from app.views.index_view import main as main_blueprint
    from app.views.auth_view import auth as auth_blueprint
    from app.views import protected as partial_blueprint
    app.register_blueprint(main_blueprint)
    app.register_blueprint(auth_blueprint)
    app.register_blueprint(partial_blueprint)

    app.jinja_env.filters["trim_float"] = trim_float
    app.jinja_env.filters["render_date"] = render_date

    for rule in app.url_map.iter_rules():
        methods = ','.join(sorted(rule.methods - {'HEAD', 'OPTIONS'}))  # Ignore HEAD/OPTIONS for clarity
        print(f"{rule.rule} -> Methods: [{methods}] Endpoint: {rule.endpoint}")

    return app
