package juton113.Atempo.service;

import jakarta.transaction.Transactional;
import juton113.Atempo.domain.dto.*;
import juton113.Atempo.domain.dto.common.HospitalInfo;
import juton113.Atempo.domain.entity.Admission;
import juton113.Atempo.domain.entity.Hospital;
import juton113.Atempo.domain.entity.Member;
import juton113.Atempo.domain.enums.AdmissionStatus;
import juton113.Atempo.domain.enums.CallResponseStatus;
import juton113.Atempo.domain.enums.CallStatus;
import juton113.Atempo.domain.enums.ErrorCode;
import juton113.Atempo.exception.CustomException;
import juton113.Atempo.repository.AdmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AdmissionService {
    private final MemberService memberService;
    private final AdmissionMessageService admissionMessageService;
    private final MlServerService mlServerService;
    private final HospitalService hospitalService;
    private final HospitalCallStatusService hospitalCallStatusService;
    private final TwilioService twilioService;

    private final AdmissionRepository admissionRepository;

    @Transactional
    public CreateAdmissionResponse createAdmissionCall(CreateAdmissionDto createAdmissionDto) {
        Member member = memberService.findByMemberId(createAdmissionDto.getMemberId());

        Admission admission = Admission.of(
                member,
                createAdmissionDto.getLocation(),
                createAdmissionDto.getSearchRadius(),
                createAdmissionDto.getPatientCondition(),
                null);

        admissionRepository.save(admission);

        // -----[Mock response used to prevent real hospital calls during development/testing]-----
        List<String> callIds = processAdmissionCall(admission);
        if (callIds.isEmpty()) {
            return CreateAdmissionResponse.builder()
                    .admissionId(admission.getAdmissionId())
                    .admissionStatus(AdmissionStatus.NO_HOSPITAL_FOUND)
                    .build();
        }

        sendMockHospitalResponses(callIds);
        //

        return CreateAdmissionResponse.builder()
                .admissionId(admission.getAdmissionId())
                .admissionStatus(AdmissionStatus.SUCCESS)
                .build();
    }

    @Transactional
    public CreateAdmissionResponse retryAdmissionCallByRadius(RetryAdmissionDto retryAdmissionDto) {
        Member member = memberService.findByMemberId(retryAdmissionDto.getMemberId());
        Long originalAdmissionId = retryAdmissionDto.getOriginalAdmissionId();
        Admission originalAdmission = admissionRepository.findById(originalAdmissionId).orElseThrow(() -> new CustomException(ErrorCode.ADMISSION_NOT_FOUND));

        Admission admission = Admission.of(
                member,
                retryAdmissionDto.getLocation(),
                retryAdmissionDto.getSearchRadius(),
                originalAdmission.getPatientCondition(),
                originalAdmissionId);

        admissionRepository.save(admission);

        // -----[Mock response used to prevent real hospital calls during development/testing]-----
        List<String> callIds = processAdmissionCall(admission);
        if (callIds.isEmpty()) {
            return CreateAdmissionResponse.builder()
                    .admissionId(admission.getAdmissionId())
                    .admissionStatus(AdmissionStatus.NO_HOSPITAL_FOUND)
                    .build();
        }

        sendMockHospitalResponses(callIds);
        //

        return CreateAdmissionResponse.builder()
                .admissionId(admission.getAdmissionId())
                .admissionStatus(AdmissionStatus.SUCCESS)
                .build();
    }

    private List<String> processAdmissionCall(Admission admission) {
        // request hospital list and ars message to ml-server
        MlCreateAdmissionRequest request = MlCreateAdmissionRequest.builder()
                .location(admission.getLocation())
                .searchRadius(admission.getSearchRadius())
                .patientCondition(admission.getPatientCondition())
                .build();
        MlCreateAdmissionResponse response = mlServerService.requestAdmissionData(request);
        // TODO: Uncomment this line and remove requestAdmissionMockData() for production
//        MlCreateAdmissionResponse response = mlServerService.requestAdmissionMockData(request);
        if (response.getHospitalList().isEmpty()) {
            return new ArrayList<>();
        }

        List<HospitalInfo> hospitalInfoList = response.getHospitalList();
        String arsMessage = response.getArsMessage();

        // create admission message entity
        CreateAdmissionMessageDto createAdmissionMessageDto = CreateAdmissionMessageDto.builder()
                .admission(admission)
                .message(arsMessage)
                .build();
        admissionMessageService.createAdmissionMessage(createAdmissionMessageDto);

        List<String> callIds = new ArrayList<>();

        // request twilio call
        for(HospitalInfo hospitalInfo : hospitalInfoList) {
            String hospitalPhoneNumber = hospitalInfo.getPhoneNumber();

//            String callId = twilioService.createCall(hospitalNumber, arsMessage);
            // TODO: Uncomment this line and remove requestAdmissionMockData() for production
            String callId = twilioService.createMockCall(hospitalPhoneNumber, arsMessage);
            callIds.add(callId);

            CreateHospitalDto createHospitalDto = CreateHospitalDto.builder()
                    .admission(admission)
                    .name(hospitalInfo.getName())
                    .phoneNumber(hospitalInfo.getPhoneNumber())
                    .address(hospitalInfo.getAddress())
                    .distance(hospitalInfo.getDistance())
                    .travelTime(hospitalInfo.getTravelTime())
                    .departments(String.join(",", hospitalInfo.getDepartments()))
                    .build();

            // create hospital entity
            Hospital hospital = hospitalService.createHospital(createHospitalDto);

            // create hospitalCallStatus entity
            CreateHospitalCallStatusDto createHospitalCallStatusDto = CreateHospitalCallStatusDto.builder()
                    .callId(callId)
                    .hospital(hospital)
                    .callStatus(CallStatus.NO_ANSWER)
                    .callResponseStatus(CallResponseStatus.NO_RESPONSE)
                    .callAttempts(0)
                    .build();

            hospitalCallStatusService.createHospitalCallStatus(createHospitalCallStatusDto);
        }

        return callIds;
    }

    private void sendMockHospitalResponses(List<String> callIds) {
        for (String callId : callIds) {
            UpdateHospitalCallStatusDto updateHospitalCallStatusDto = UpdateHospitalCallStatusDto.builder()
                    .callId(callId)
                    .responseDigit("1")
                    .build();

            hospitalCallStatusService.updateHospitalCallStatus(updateHospitalCallStatusDto);
        }
    }
}
