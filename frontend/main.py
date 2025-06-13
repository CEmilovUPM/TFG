from app import create_app
from app.backend_client import Backend
import logging

app = create_app()

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    app.run(debug=True)