package br.edu.ulbra.election.party.service;

import br.edu.ulbra.election.party.repository.PartyRepository;
import feign.FeignException;
import br.edu.ulbra.election.party.client.CandidateClientService;
import br.edu.ulbra.election.party.exception.GenericOutputException;
import br.edu.ulbra.election.party.input.v1.PartyInput;
import br.edu.ulbra.election.party.model.Party;
import br.edu.ulbra.election.party.output.v1.GenericOutput;
import br.edu.ulbra.election.party.output.v1.PartyOutput;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.lang.reflect.Type;
import java.util.List;

@Service
public class PartyService {

	private final PartyRepository partyRepository;
	private final ModelMapper modelMapper;
	private final CandidateClientService candidateClientService;

	private static final String MESSAGE_INVALID_ID = "Invalid id";
	private static final String MESSAGE_PARTY_NOT_FOUND = "Party not found";

	@Autowired
	public PartyService(PartyRepository partyRepository, ModelMapper modelMapper,
			CandidateClientService candidateClientService) {
		this.partyRepository = partyRepository;
		this.modelMapper = modelMapper;
		this.candidateClientService = candidateClientService;
	}

	public List<PartyOutput> getAll() {
		Type partyOutputListType = new TypeToken<List<PartyOutput>>() {
		}.getType();
		return modelMapper.map(partyRepository.findAll(), partyOutputListType);
	}

	public PartyOutput create(PartyInput partyInput) {
		validateInput(partyInput, partyRepository);
		Party party = modelMapper.map(partyInput, Party.class);
		party = partyRepository.save(party);
		return modelMapper.map(party, PartyOutput.class);
	}

	public PartyOutput getById(Long partyId) {
		if (partyId == null) {
			throw new GenericOutputException(MESSAGE_INVALID_ID);
		}

		Party party = partyRepository.findById(partyId).orElse(null);
		if (party == null) {
			throw new GenericOutputException(MESSAGE_PARTY_NOT_FOUND);
		}

		return modelMapper.map(party, PartyOutput.class);
	}

	public PartyOutput update(Long partyId, PartyInput partyInput) {
		if (partyId == null) {
			throw new GenericOutputException(MESSAGE_INVALID_ID);
		}
		validateInput(partyInput, partyRepository);

		Party party = partyRepository.findById(partyId).orElse(null);
		if (party == null) {
			throw new GenericOutputException(MESSAGE_PARTY_NOT_FOUND);
		}

		party.setCode(partyInput.getCode());
		party.setName(partyInput.getName());
		party.setNumber(partyInput.getNumber());
		party = partyRepository.save(party);
		return modelMapper.map(party, PartyOutput.class);
	}

	public GenericOutput delete(Long partyId) {
		if (partyId == null) {
			throw new GenericOutputException(MESSAGE_INVALID_ID);
		}

		verifyCandidate(partyId);

		Party party = partyRepository.findById(partyId).orElse(null);
		if (party == null) {
			throw new GenericOutputException(MESSAGE_PARTY_NOT_FOUND);
		}

		partyRepository.delete(party);

		return new GenericOutput("Party deleted");
	}

	private void verifyCandidate(Long partyId) {

		try {
			candidateClientService.verifyParty(partyId);
			throw new GenericOutputException("Already exists candidates.");
		} catch (FeignException e) {
			if (e.status() != 500) {
				throw new GenericOutputException("Error");
			}
		}
	}

	private void validateInput(PartyInput partyInput, PartyRepository partyRepository) {
		if (StringUtils.isBlank(partyInput.getCode())) {
			throw new GenericOutputException("Invalid code");
		} else {
			if (Party.verifyCode(partyInput.getCode(), partyRepository)) {
				throw new GenericOutputException("This code already used.");
			}
		}

		if (partyInput.getNumber() == null || partyInput.getNumber().toString().length() != 2) {
			throw new GenericOutputException("Invalid number");
		} else {
			if (Party.verifyNumber(partyInput.getNumber(), partyRepository)) {
				throw new GenericOutputException("This number already used.");
			}
		}

		if (StringUtils.isBlank(partyInput.getName()) || partyInput.getName().trim().replace(" ", "").length() < 5) {
			throw new GenericOutputException("Invalid name");
		}
	}

}
