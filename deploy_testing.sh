
#this file has to be run in the linux machine
kubectl delete -f backend/deployment_files/test/deploy.yaml --namespace=testing
kubectl delete -f database/test/deploy.yaml --namespace=testing

git pull;
chmod +x mvnw;

kubectl apply -f database/test/deploy.yaml --namespace=testing
kubectl apply -f backend/deployment_files/test/deploy.yaml --namespace=testing