package org.stepik.android.adaptive.api;

import org.stepik.android.adaptive.data.model.Unit;

import java.util.List;

public class UnitsResponse {
    private List<Unit> units;

    public Unit getTopUnit() {
        if (units != null && units.size() > 0)
            return units.get(0);
        else
            return null;
    }
}
