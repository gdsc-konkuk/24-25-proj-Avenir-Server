package juton113.Atempo.domain.dto;

import juton113.Atempo.domain.entity.Admission;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateAdmissionMessageDto {
    private Admission admission;
    private String message;
}
