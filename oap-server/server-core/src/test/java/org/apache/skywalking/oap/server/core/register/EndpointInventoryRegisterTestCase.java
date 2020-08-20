package org.apache.skywalking.oap.server.core.register;

import org.apache.skywalking.oap.server.core.register.service.EndpointInventoryRegister;
import org.junit.Assert;
import org.junit.Test;

public class EndpointInventoryRegisterTestCase {

    @Test
    public void testCutDownEndpoint() {
        EndpointInventoryRegister register = new EndpointInventoryRegister(null);
        Assert.assertEquals("/endpoint_inventory/type/1070_/fresh/necessary/goods", register.cutDownEndpoint("/endpoint_inventory/type/1070_%2Ffresh%2Fnecessary%2Fgoods%2F1000154880%252C1000155136%252C1000154881%252C1000155137%252C1000155138%252C1000154883%252C1000155139%252C1000154884%252C1000155140%252C1000154885%252C1000155141%252C1000154886%252C1000155142%252C1000154887%252C1000155143%252C1000154888%252C1000155144%252C1000154889%252C1000154890%252C1000154891%252C1000154892%252C1000154893%252C1000154894%252C1000154895%252C1000154896%252C1000154897%252C1000154898%252C1000154899%252C1000154900%252C1000154901%252C1000154902%252C1000154903%252C1000154904%252C1000154905%252C1000154906%252C1000154907%252C1000154908%252C1000154909%252C1000154912%252C1000154913%252C1000154914%252C1000154915%252C1000154917%252C1000154922%252C1000154924%252C1000154669%252C1000154925%252C1000154926%252C1000154671%252C1000154927%252C1000154672%252C1000154928%252C1000154673%252C1000154674%252C1000154930%252C1000154675%252C1000154931%252C1000154676%252C1000154677%252C1000154933%252C1000154678%252C1000154934%252C1000154679%252C1000154935%252C1000154680%252C1000154937%252C1000154682%252C1000154938%252C1000154683%252C1000154939%252C1000154684%252C1000154685%252C1000154941%252C1000154686%252C1000154942%252C1000154943%252C1000154944%252C1000154689%252C1000154945%252C1000154690%252C1000154946%252C1000154691%252C1000154947%252C1000154692%252C1000154948%252C1000154949%252C1000154694%252C1000154950%252C1000154695%252C1000154951%252C1000154696%252C1000154697%252C1000154953%252C1000154698%252C1000154954%252C1000154699%252C1000154955%252C1000154700%252C1000154956%252C1000154701%252C1000154957%252C1000154702%252C1000154958%252C1000154703%252C1000154959%252C1000154704%252C1000154960%252C1000154705%252C1000154961%252C1000154706%252C1000154962%252C1000154707%252C1000154708%252C1000154964%252C1000154709%252C1000154965%252C1000154966%252C1000154711%252C1000154967%252C1000154712%252C1000154968%252C1000154714%252C1000154970%252C1000154715%252C1000154971%252C1000154716%252C1000154972%252C1000154717%252C1000154973%252C1000154718%252C1000154974%252C1000154719%252C1000154720%252C1000154976%252C1000154721%252C1000154977%252C1000154722%252C1000154978%252C1000154723%252C1000154979%252C1000154724%252C1000154980%252C1000154725%252C1000154981%252C1000154726%252C1000154982%252C1000154727%252C1000154983%252C1000154728%252C1000154984%252C1000154729%252C1000154730%252C1000154986%252C1000154987%252C1000154988%252C1000154733%252C1000154989%252C1000154734%252C1000154990%252C1000154735%252C1000154991%252C1000154736%252C1000154992%252C1000154737%252C1000154993%252C1000154738%252C1000154739%252C1000154995%252C1000154740%252C1000154996%252C1000154997%252C1000154998%252C1000154999%252C1000154744%252C1000155000%252C1000154745%252C1000155001%252C1000154746%252C1000155002%252C1000154747%252C1000154748%252C1000155004%252C1000155005%252C1000155006%252C1000154751%252C1000155007%252C1000154752%252C1000155008%252C1000154753%252C1000155009%252C1000154754%252C1000155010%252C1000154755%252C1000154756%252C1000154757%252C1000155013%252C1000154758%252C1000155014%252C1000154759%252C1000155015%252C1000154760%252C1000155016%252C1000154761%252C1000155017%252C1000154762%252C1000155018%252C1000154763%252C1000154764%252C1000155021%252C1000154766%252C1000155022%252C1000154767%252C1000155023%252C1000154768%252C1000155024%252C1000154769%252C1000155025%252C1000154770%252C1000155026%"));
        Assert.assertEquals("/endpoint_inventory/type/1070_/fresh/necessary/goods", register.cutDownEndpoint("/endpoint_inventory/type/1070_/fresh/necessary/goods/54880%252C1000155136%252C1000154881%252C1000155137%252C1000155138%252C1000154883%252C1000155139%252C1000154884%252C1000155140%252C1000154885%252C1000155141%252C1000154886%252C1000155142%252C1000154887%252C1000155143%252C1000154888%252C1000155144%252C1000154889%252C1000154890%252C1000154891%252C1000154892%252C1000154893%252C1000154894%252C1000154895%252C1000154896%252C1000154897%252C1000154898%252C1000154899%252C1000154900%252C1000154901%252C1000154902%252C1000154903%252C1000154904%252C1000154905%252C1000154906%252C1000154907%252C1000154908%252C1000154909%252C1000154912%252C1000154913%252C1000154914%252C1000154"));
    }
}