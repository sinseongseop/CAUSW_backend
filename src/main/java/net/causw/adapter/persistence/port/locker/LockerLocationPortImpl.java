package net.causw.adapter.persistence.port.locker;

import net.causw.adapter.persistence.locker.LockerLocation;
import net.causw.adapter.persistence.port.mapper.DomainModelMapper;
import net.causw.adapter.persistence.repository.LockerLocationRepository;
import net.causw.application.spi.LockerLocationPort;
import net.causw.domain.model.locker.LockerLocationDomainModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class LockerLocationPortImpl extends DomainModelMapper implements LockerLocationPort {
    private final LockerLocationRepository lockerLocationRepository;

    public LockerLocationPortImpl(LockerLocationRepository lockerLocationRepository) {
        this.lockerLocationRepository = lockerLocationRepository;
    }

    @Override
    public Optional<LockerLocationDomainModel> findById(String id) {
        return this.lockerLocationRepository.findById(id).map(this::entityToDomainModel);
    }

    @Override
    public Optional<LockerLocationDomainModel> findByName(String name) {
        return this.lockerLocationRepository.findByName(name).map(this::entityToDomainModel);
    }

    @Override
    public List<LockerLocationDomainModel> findAll() {
        return this.lockerLocationRepository.findAll()
                .stream()
                .map(this::entityToDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public LockerLocationDomainModel create(LockerLocationDomainModel lockerLocationDomainModel) {
        return this.entityToDomainModel(
                this.lockerLocationRepository.save(LockerLocation.from(lockerLocationDomainModel))
        );
    }

    @Override
    public Optional<LockerLocationDomainModel> update(String id, LockerLocationDomainModel lockerLocationDomainModel) {
        return this.lockerLocationRepository.findById(id).map(
                srcLockerLocation -> {
                    srcLockerLocation.update(lockerLocationDomainModel.getName());

                    return this.entityToDomainModel(this.lockerLocationRepository.save(srcLockerLocation));
                }
        );
    }

    @Override
    public void delete(LockerLocationDomainModel lockerLocationDomainModel) {
        this.lockerLocationRepository.delete(LockerLocation.from(lockerLocationDomainModel));
    }
}
