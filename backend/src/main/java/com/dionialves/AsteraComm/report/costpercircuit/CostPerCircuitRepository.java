package com.dionialves.AsteraComm.report.costpercircuit;

import com.dionialves.AsteraComm.call.Call;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CostPerCircuitRepository extends JpaRepository<Call, Long> {

    @Query("""
            SELECT new com.dionialves.AsteraComm.report.costpercircuit.CallCostReportRow(
                c.number,
                cu.name,
                COUNT(ca.id),
                COALESCE(SUM(ca.billSeconds), 0L),
                COALESCE(SUM(ca.cost), 0)
            )
            FROM Call ca
            JOIN ca.circuit c
            JOIN c.customer cu
            WHERE ca.callStatus = com.dionialves.AsteraComm.call.CallStatus.PROCESSED
              AND EXTRACT(MONTH FROM ca.callDate) = :month
              AND EXTRACT(YEAR  FROM ca.callDate) = :year
            GROUP BY c.number, cu.name
            ORDER BY cu.name ASC, c.number ASC
            """)
    List<CallCostReportRow> findCallCostByPeriod(@Param("month") int month,
                                                 @Param("year") int year);
}
