package org.drools.example.api.dynamic.service;

import javax.annotation.PostConstruct;

import org.drools.core.ClassObjectFilter;
import org.drools.example.api.model.Message;
import org.kie.api.KieServices;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.stereotype.Service;

@Service
public class MyService {

	private KieContainer kContainer;

	@PostConstruct
	public void setUp() {
		KieServices ks = KieServices.Factory.get();

		ReleaseId releaseId = ks.newReleaseId("my-group", "my-kjar", "0.0.1-SNAPSHOT");

		kContainer = ks.newKieContainer(releaseId);
		KieScanner kScanner = ks.newKieScanner(kContainer);

		// Start the KieScanner polling the Maven repository every 10 seconds
		kScanner.start(10000L);
	}

	public String run() {
		KieSession kSession = kContainer.newKieSession("ksession1");
		kSession.setGlobal("out", System.out);

		kSession.insert(new Message("Dave", "Hello, HAL. Do you read me, HAL?"));
		kSession.fireAllRules();

		String result = null;

		for (Object msg : kSession.getObjects(new ClassObjectFilter(Message.class))) {
			if (((Message) msg).getName().equals("HAL")) {
				result = ((Message) msg).getText();
			}
		}

		return result;
	}

}
