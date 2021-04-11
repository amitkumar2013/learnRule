package com.example.rules.config;

import java.io.IOException;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.ReleaseId;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SpringDroolsConfig {

	private static final String RULES_PATH = "rules/drools/";
	// thread-safe singleton
	private final KieServices kieService = KieServices.Factory.get();

	/**
	 * Read the drool files.
	 */
	@Bean
	public KieFileSystem kieFileSystem() throws IOException {
		KieFileSystem kieFileSystem = null;
		try {
			kieFileSystem = kieService.newKieFileSystem();
			ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
			Resource[] ruleFiles = resourcePatternResolver.getResources("classpath*:" + RULES_PATH + "**/*.drl");
			for (Resource file : ruleFiles) {
				kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_PATH + file.getFilename(), "UTF-8"));
			}
			log.info("File System loaded with {} file!", ruleFiles.clone().length);
		} catch (Exception e) {
			log.error("Failed to load drool files!");
			e.printStackTrace();
		}
		return kieFileSystem;
	}

	/**
	 * Every rule in Drools belongs to a rule set and the application requires a
	 * KieContainer to execute these rules against an object.
	 */
	@Bean
	public KieContainer kieContainer() throws IOException {

		KieContainer newKieContainer = null;
		// Adding KieModule (container for set of KBase(s)) to repository(singleton) and
		// matching the releaseId
		try {
			KieRepository kieRepository = kieService.getRepository();
			kieRepository.addKieModule(new KieModule() {
				public ReleaseId getReleaseId() {
					ReleaseId defaultReleaseId = kieRepository.getDefaultReleaseId();
					defaultReleaseId = kieService.newReleaseId("com.example", "ruleJar", "LATEST");
					return defaultReleaseId;
				}
			});

			/*
			 * The Knowledge Builder is responsible for taking source files, such as a .drl
			 * file, a .bpmn2 file or an .xls file, and turning them into a Knowledge
			 * Package of "rule and process definitions" which a KBase can consume.
			 */
			KieBuilder kieBuilder = kieService.newKieBuilder(kieFileSystem());
			kieBuilder.buildAll();
			/*
			 * Knowledge Base is the heart of the rules engine to make decisions. Create a
			 * Knowledge Session(handler from KBase) to interact with Drools Engine.
			 */
			// Optionally one can use Module instead of Repository viz. :
			// kieService.newKieContainer(kieBuilder.getKieModule().getReleaseId());
			/*
			 * KieContainer - Container for all the KieBases of a given KieModule/Repository
			 * of modules.
			 */
			newKieContainer = kieService.newKieContainer(kieRepository.getDefaultReleaseId());

			// Keep looking for new releases.
			KieScanner kieScanner = kieService.newKieScanner(newKieContainer, RULES_PATH);
			kieScanner.start(1000L);
			log.info("KieContainer ready with modules/base as follows: {}", kieBuilder.getResults());
		} catch (Exception e) {
			log.error("Failed to build knowledge bases as KIE Container!");
			e.printStackTrace();
		}

		return newKieContainer;
	}

	@Bean
	@ConditionalOnMissingBean(KieSession.class)
	public KieSession kieSession() throws IOException {
		KieSession kieSession = null;
		try {
			kieSession = kieContainer().newKieSession();
			//kieSession.setGlobal(... If required

			kieSession.addEventListener(new RuleRuntimeEventListener() {
				@Override
				public void objectInserted(ObjectInsertedEvent event) {
					log.debug("Object inserted {}", event.getObject().toString());
				}

				@Override
				public void objectUpdated(ObjectUpdatedEvent event) {
					log.debug("Object was updated with {}", event.getObject().toString());
				}

				@Override
				public void objectDeleted(ObjectDeletedEvent event) {
					log.debug("Object retracted {}", event.getOldObject().toString());
				}
			});
			log.info("KieContainer ready with session: {} for loaded modules/base!", kieSession.getIdentifier());
		} catch (Exception e) {
			log.error("Failed to build session on Knowledge bases using KIE Container!");
			e.printStackTrace();
		}

		return kieSession;
	}

}
