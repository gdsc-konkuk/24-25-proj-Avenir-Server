package juton113.Avenir.service;

import jakarta.transaction.Transactional;
import juton113.Avenir.domain.dto.CreateHospitalDto;
import juton113.Avenir.domain.entity.Hospital;
import juton113.Avenir.repository.HospitalRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class HospitalService {
    private final HospitalRepository hospitalRepository;

    @Transactional
    public Hospital createHospital(CreateHospitalDto createHospitalDto) {
        Hospital hospital = Hospital.builder()
                .admission(createHospitalDto.getAdmission())
                .phoneNumber(createHospitalDto.getPhoneNumber())
                .address(createHospitalDto.getAddress())
                .distance(createHospitalDto.getDistance())
                .travelTime(createHospitalDto.getTravelTime())
                .detail(createHospitalDto.getDetail())
                .build();

        return hospitalRepository.save(hospital);
    }
}
