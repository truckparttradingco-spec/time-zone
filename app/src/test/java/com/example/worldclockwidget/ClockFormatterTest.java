package com.example.worldclockwidget;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

public class ClockFormatterTest {
    @Test public void allSelectableCountriesHaveUniqueIdsAndZones() {
        Set<String> ids = new HashSet<>();
        for (CountryCatalog.Country c : CountryCatalog.ALL) {
            assertTrue(c.id, ids.add(c.id));
            assertNotNull(c.zone);
            assertFalse(c.zone.trim().isEmpty());
        }
        assertEquals(17, CountryCatalog.ALL.size());
    }

    @Test public void allCountriesFormatTimeAndCalendarDate() {
        long instant = 1793606400000L;
        for (CountryCatalog.Country c : CountryCatalog.ALL) {
            assertNotNull(c.id, ClockFormatter.time(c, instant));
            assertFalse(c.id, ClockFormatter.time(c, instant).isEmpty());
            assertNotNull(c.id, ClockFormatter.date(c, instant));
            assertFalse(c.id, ClockFormatter.date(c, instant).isEmpty());
        }
    }

    @Test public void berlinAndTehranUseDifferentTimeZones() {
        CountryCatalog.Country berlin = CountryCatalog.byId("germany");
        CountryCatalog.Country tehran = CountryCatalog.byId("tehran");
        assertNotEquals(berlin.zone, tehran.zone);
        assertTrue(ClockFormatter.offset(berlin, 1793606400000L).startsWith("UTC"));
        assertTrue(ClockFormatter.offset(tehran, 1793606400000L).startsWith("UTC"));
    }
}
