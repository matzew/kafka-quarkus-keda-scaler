# kafka-quarkus-keda-scaler

Sample project that builds a simple `QuarkusConsumer`, based on [Quarkus](https://quarkus.io/) reading from a topic like:

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: my-topic
  labels:
    strimzi.io/cluster: my-cluster
spec:
  partitions: 10
  replicas: 3
  config:
    retention.bytes: 53687091200
    retention.ms: 36000000
```

We now use [KEDA](./keda-scalers.yaml) to dynamically scale our consumers, based on the consumer group for the topic.

The application is defined as a normal Kubernetes [deployment](./k8s.yaml).

## The load

For generating some load, a batch like below can be used:

```
apiVersion: batch/v1
kind: Job
metadata:
  labels:
    app: kafka-producer-job-notls-noacks
  name: kafka-producer-job-notls-noacks
  namespace: kafka
spec:
  parallelism: 5
  completions: 5
  backoffLimit: 1
  template:
    metadata:
      name: kafka-perf-producer
      labels:
        app: kafka-perf-producer
    spec:
      restartPolicy: Never
      containers:
      - name: kafka-perf-producer
        image: quay.io/strimzi/kafka:0.24.0-kafka-2.7.1
        command: [ "bin/kafka-producer-perf-test.sh" ]
        args: [ "--topic", "my-topic", "--throughput", "10000000", "--num-records", "1000000", "--producer-props", "bootstrap.servers=my-cluster-kafka-bootstrap:9092", "--record-size", "1000" ]
        volumeMounts:
        - name: strimzi-ca
          readOnly: true
          mountPath: "/etc/strimzi"
        env:
        - name: CA_PASSWORD
          valueFrom:
            secretKeyRef:
              name: my-cluster-cluster-ca-cert
              key: ca.password
      volumes:
      - name: strimzi-ca
        secret:
          secretName: my-cluster-cluster-ca-cert
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - kafka-perf-producer
              topologyKey: kubernetes.io/hostname
```

## Scaled consumers

After the initial count of `one` replica from the deployment, once the above load kicks in, KEDA dynamically scales the app to `10` replicas, see:

```
k get pods           
NAME                                     READY   STATUS    RESTARTS   AGE
kafka-quarkus-consumer-fbbdb4c57-6d77c   1/1     Running   0          50s
kafka-quarkus-consumer-fbbdb4c57-bfjxj   1/1     Running   0          50s
kafka-quarkus-consumer-fbbdb4c57-bp6th   1/1     Running   0          20s
kafka-quarkus-consumer-fbbdb4c57-ccb6q   1/1     Running   0          20s
kafka-quarkus-consumer-fbbdb4c57-g7pr5   1/1     Running   0          35s
kafka-quarkus-consumer-fbbdb4c57-j6289   1/1     Running   0          35s
kafka-quarkus-consumer-fbbdb4c57-ll789   1/1     Running   2          10m
kafka-quarkus-consumer-fbbdb4c57-qrp2l   1/1     Running   0          50s
kafka-quarkus-consumer-fbbdb4c57-s6w7f   1/1     Running   0          35s
kafka-quarkus-consumer-fbbdb4c57-scljw   1/1     Running   0          35s
```

**NOTE:** This is a simple POC/demo, and nothing special for rebalancing was done!