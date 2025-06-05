package software.sailes.carrental.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarChargeRepository extends CrudRepository<CarCharge, CarChargeId> {

    @Query("SELECT c FROM CarCharge c WHERE c.carId = :carId")
    List<CarCharge> findAllByCarId(@Param("carId") CarId carId);
}
