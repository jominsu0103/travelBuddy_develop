package com.github.travelbuddy.routes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "routes_day")
public class RouteDayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "routes_id", nullable = false)
    private RouteEntity route;

    @Column(name = "day", nullable = false)
    private Date day;

    @OneToMany(mappedBy = "routeDay",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    private List<RouteDayPlaceEntity> routeDayPlaces;
}
