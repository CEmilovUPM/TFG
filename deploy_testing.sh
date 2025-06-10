
#this file has to be run in the linux machine
kubectl delete -f backend/deployment_files/test/deploy.yaml --namespace=testing
kubectl delete -f database/test/deploy.yaml --namespace=testing

git restore .;
git pull;
echo cloned repository;
chmod +x mvnw;
chmod +x deploy_testing.sh

kubectl apply -f database/test/deploy.yaml --namespace=testing
kubectl apply -f backend/deployment_files/test/deploy.yaml --namespace=testing