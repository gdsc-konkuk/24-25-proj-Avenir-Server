package juton113.Atempo.domain.dto;

import juton113.Atempo.domain.entity.Hospital;
import juton113.Atempo.domain.enums.CallResponseStatus;
import juton113.Atempo.domain.enums.CallStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateHospitalCallStatusDto {
    private String callId;
    private Hospital hospital;
    CallStatus callStatus;
    CallResponseStatus callResponseStatus;
    int callAttempts;
}
