package org.drools.example.api.dynamic.controller;

import org.drools.example.api.dynamic.service.MyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

	@Autowired
	private MyService myService;
	
	@GetMapping("/dynamic-rule")
	public String test() {
		return myService.run();
	}
	
}
