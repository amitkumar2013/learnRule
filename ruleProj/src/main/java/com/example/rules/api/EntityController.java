package com.example.rules.api;

import java.util.concurrent.ThreadLocalRandom;

import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.rules.model.Policy;
import com.example.rules.model.Result;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class EntityController {

	@Autowired
	private KieSession kieSession;

	@GetMapping("/entity/{id}")
	public String executeRule(@PathVariable long id) {
		Result result = new Result("okay");
		kieSession.setGlobal("result", result);
		Policy policy = new Policy(new Long(ThreadLocalRandom.current().nextLong(500, 5000)));
		kieSession.insert(policy);
		kieSession.fireAllRules();
		//kieSession.dispose(); // - destroys session; can be reloaded if persisted.
		log.info("Result {} for {}", result.getValue(), policy.getId());
		return result.getValue();
	}
}
