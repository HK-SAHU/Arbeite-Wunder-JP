package com.jobportal.utility;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.jobportal.entity.Sequence;
import com.jobportal.exception.JobPortalException;

@Component
public class Utilities {
	private static MongoOperations mongoOperation;

	@Autowired
	public void setMongoOperation(MongoOperations mongoOperation) {
		Utilities.mongoOperation = mongoOperation;
	}

	public static Long getNextSequenceId(String key) throws JobPortalException {
		Query query = new Query(Criteria.where("_id").is(key));
		Update update = new Update();
		update.inc("seq", 1);
		FindAndModifyOptions options = new FindAndModifyOptions();
		options.returnNew(true);
		Sequence seqId = mongoOperation.findAndModify(query, update, options, Sequence.class);
		if (seqId == null) {
			// Try to initialize the sequence if it doesn't exist
			try {
				initializeSequence(key);
				// Try again after initialization
				seqId = mongoOperation.findAndModify(query, update, options, Sequence.class);
				if (seqId == null) {
					throw new JobPortalException("Unable to get sequence id for key : " + key);
				}
			} catch (Exception e) {
				throw new JobPortalException("Unable to get sequence id for key : " + key);
			}
		}

		return seqId.getSeq();
	}

	public static void initializeSequence(String key) {
		// Check if sequence already exists
		Query query = new Query(Criteria.where("_id").is(key));
		if (mongoOperation.exists(query, Sequence.class)) {
			return; // Sequence already exists
		}
		
		// Create new sequence with initial value 1
		Sequence sequence = new Sequence();
		sequence.setId(key);
		sequence.setSeq(1L);
		mongoOperation.save(sequence);
		System.out.println("Initialized sequence: " + key);
	}

	public static String generateOTP() {
		StringBuilder otp = new StringBuilder();
		SecureRandom secureRandom = new SecureRandom();
		for (int i = 0; i < 6; i++) {
			otp.append(secureRandom.nextInt(10));
		}
		return otp.toString();
	}
}
