package org.SouthMillion.dto.pet;

import lombok.Data;

import java.util.List;

@Data
public class AddMinMaxDTO {
    private int seq;
    private int add_type;
    private int add_min;
    private int add_max;
    private int rate;
    private List<PetAttDTO> range_a;
    private List<PetAttDTO> range_b;
    private List<PetAttDTO> range_c;
    private List<PetAttDTO> range_d;
    private List<PetAttDTO> range_e;

}