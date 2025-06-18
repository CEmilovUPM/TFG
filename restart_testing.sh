git restore .;
git pull;
echo cloned repository;
chmod +x mvnw;
chmod +x deploy_testing.sh
chmod +x restart_testing.sh

kubectl rollout restart deployment flask-frontend -n testing
kubectl rollout restart deployment graph-microservice -n testing
