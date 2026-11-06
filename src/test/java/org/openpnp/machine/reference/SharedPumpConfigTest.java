package org.openpnp.machine.reference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Nozzle;

class SharedPumpConfigTest {
    @Test void configurationLoadsAndEitherNozzleKeepsPumpOn() throws Exception {
        Path temp = Files.createTempDirectory("shared-pump-config");
        Path source = Paths.get("src/test/resources/config/shared-pump");
        Files.copy(source.resolve("machine.xml"), temp.resolve("machine.xml"));
        Files.copy(source.resolve("vision-settings.xml"), temp.resolve("vision-settings.xml"));
        Configuration.initialize(temp.toFile());
        Configuration.get().load();
        ReferenceHead loaded = (ReferenceHead) Configuration.get().getMachine().getDefaultHead();
        assertNotNull(loaded.getPumpActuator());
        assertEquals("SharedPump", loaded.getPumpActuator().getName());
        ReferenceHead head = new ReferenceHead();
        Actuator pump = mock(Actuator.class);
        Nozzle n1 = mock(Nozzle.class);
        Nozzle n2 = mock(Nozzle.class);
        head.addNozzle(n1);
        head.addNozzle(n2);
        head.setPumpActuator(pump);
        head.actuatePumpRequest(n1, true);
        verify(pump).actuate(true);
        when(n2.getPart()).thenReturn(new Part("held"));
        head.actuatePumpRequest(n1, false);
        verify(pump, never()).actuate(false);
        when(n2.getPart()).thenReturn(null);
        head.actuatePumpRequest(n2, false);
        verify(pump).actuate(false);
        reset(pump);
        when(n1.getPart()).thenReturn(new Part("held-other"));
        head.actuatePumpRequest(n2, false);
        verify(pump, never()).actuate(false);
    }
}
