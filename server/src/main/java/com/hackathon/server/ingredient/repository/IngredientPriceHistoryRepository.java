package com.hackathon.server.ingredient.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.hackathon.server.ingredient.entity.IngredientPriceHistory;

public interface IngredientPriceHistoryRepository extends JpaRepository<IngredientPriceHistory, IngredientPriceHistory.Key> {

	@Query("select max(h.observedDate) from IngredientPriceHistory h")
	Optional<LocalDate> findLatestObservedDate();

	List<IngredientPriceHistory> findByObservedDate(LocalDate observedDate);

	/** 같은 원자재의 직전 관측일 가격을 다시 채운다(V22와 같은 계산). 바뀐 행만 고친다. */
	@Modifying
	@Query(nativeQuery = true, value = """
			with previous_prices as (
			    select ingredient_id, observed_date, lag(price) over (partition by ingredient_id order by observed_date) as price
			    from ingredient_price_history)
			update ingredient_price_history history set previous_day_price = previous_prices.price
			from previous_prices
			where history.ingredient_id = previous_prices.ingredient_id and history.observed_date = previous_prices.observed_date
			    and history.previous_day_price is distinct from previous_prices.price""")
	int refreshPreviousPrices();
}
