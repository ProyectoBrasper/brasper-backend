package com.brasper.platform.staff.application.queryservices;

import com.brasper.platform.shared.domain.model.valueobjects.PageSpec;
import com.brasper.platform.shared.domain.model.valueobjects.PagedList;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.model.entities.FixedCostPayment;
import com.brasper.platform.staff.domain.repositories.FixedCostRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FixedCostQueryService {
    private final FixedCostRepository costs;
    public FixedCostQueryService(FixedCostRepository costs) { this.costs = costs; }
    /** The active filter is optional; oldest first. */
    public List<FixedCost> findAll(Boolean active) {
        return withWeeks(costs.filter(active, 0, Integer.MAX_VALUE));
    }
    /** Same filter as {@link #findAll}, one page of it plus the total that match. */
    public PagedList<FixedCost> findPage(Boolean active, PageSpec page) {
        return new PagedList<>(withWeeks(costs.filter(active, page.page(), page.size())), costs.countFiltered(active));
    }
    private static List<FixedCost> withWeeks(List<FixedCost> result) {
        result.forEach(cost -> cost.getExpectedWeeks());
        return result;
    }
    public Optional<FixedCost> findById(Long id) {
        var result = costs.findById(id);
        result.ifPresent(cost -> cost.getExpectedWeeks());
        return result;
    }
    public record FixedCostPaymentView(Long fixedCostId, FixedCostPayment payment) {}
    public List<FixedCostPaymentView> findPaymentsByPeriod(Period period) {
        return costs.findAll().stream().flatMap(cost -> cost.getPayments().stream()
                .filter(payment -> payment.getPeriod().equals(period))
                .map(payment -> new FixedCostPaymentView(cost.getId(), payment))).toList();
    }
}
