package com.example.patientservice.kafka;


import com.example.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaProducer {


  private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
  private final KafkaTemplate<String, byte[]> kafkaTemplate;

  public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void sendMessage(Patient patient) {
    PatientEvent patientEvent = PatientEvent.newBuilder().setPatientID(patient.getId().toString())
      .setEmail(patient.getEmail())
      .setName(patient.getName())
      .setEventType("PatientCreated")
      .build();



    try {
      kafkaTemplate.send("patient", patientEvent.toByteArray());
      log.info("Succssfully Sent Kafak Message", patientEvent);

    } catch (Exception e) {
      log.error("Error sending patient event to kafka: {}", patientEvent);
    }


  }


}
