package tutorial1;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerDemoWithThread {
    public static void main(String[] args) {
        new ConsumerDemoWithThread().run();
    }

    private ConsumerDemoWithThread(){

    }

    private void run(){
        Logger logger = LoggerFactory.getLogger(ConsumerDemoWithThread.class.getName());

        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "my-sixth-application";
        String topic = "first_topic";
        // latch for dealing with multiple threads
        CountDownLatch latch = new CountDownLatch(1);

        // Create the consumer runnable
        logger.info("Creating the consumer thread");
        Runnable myConsumerRunnable = new ConsumerRunnable(
                bootstrapServers,
                groupId,
                topic,
                latch);

        // Start the thread
        Thread myThread = new Thread(myConsumerRunnable);
        myThread.start();

        // Add the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Caught shutdown hook");
            ((ConsumerRunnable) myConsumerRunnable).shutdown();
            try {
                latch.await();
            } catch(InterruptedException e){
                e.printStackTrace();
            }
            logger.info("Application has exited");
        }));

        try{
            latch.await(); // dont want our application exit right away, make us wait all the way until the application is over
        } catch(InterruptedException e){
            logger.error("Application got interrupted", e);
        } finally{
            logger.info("Application is closing");
        }

    }

    // Create a new class for Kafka consumer
    public class ConsumerRunnable implements Runnable{

        private CountDownLatch latch;
        private KafkaConsumer<String, String> consumer;
        private Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class.getName());

        public ConsumerRunnable(String bootstrapServers,
                              String groupId,
                              String topic,
                              CountDownLatch latch){  // latch deals with concurrency (shut down application correctly

            this.latch = latch;

            // Create consumer properties
            Properties properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            // Create consumer
            consumer = new KafkaConsumer<String, String>(properties);

            // Subscribe consumer to our topic(s)
            consumer.subscribe(Arrays.asList(topic));  // same as consumer.subscribe(Collections.singleton(topic));
        }

        @Override
        public void run() {
            // Poll for new data
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));  // new in Kafka 2.0.0
                    for (ConsumerRecord<String, String> record : records) {
                        logger.info("Key: " + record.key() + ", Value: " + record.value());
                        logger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                    }
                }
            } catch(WakeupException e){
                logger.info("Received shutdown signal!");
            } finally{
                consumer.close();
                // tell the main code we are done with the consumer
                latch.countDown();
            }

        }

        public void shutdown(){
            // the wakeup() method is a special method to interrupt consumer.poll()
            // it will throw the exception WakeUpException
            consumer.wakeup();
        }
    }
}

