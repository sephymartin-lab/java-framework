/*
 * Copyright 2022-2026 sephy.top
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.sephy.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.jupiter.api.Test;

class HttpClientUtilsTest {

    HttpClient httpClient = HttpClients.createDefault();

    @Test
    public void testCurlToHttpRequest() throws Exception {
        String curlCommand =
            """
                curl 'https://www.baidu.com/sugrec?pre=1&p=3&ie=utf-8&json=1&prod=pc&from=pc_web&sugsid=61027,61216,61361,60853,61530,61534,61608,61721,61729&wd=curl&his=%5B%7B%22time%22%3A1713705986%2C%22kw%22%3A%22%E5%A4%A9%E6%B5%99%E5%A4%A9%E4%B8%8B%E5%95%86%E5%B8%AE%E7%A7%91%E6%8A%80%E8%82%A1%E4%BB%BD%E6%9C%89%E9%99%90%E5%85%AC%E5%8F%B8%208637%22%7D%2C%7B%22time%22%3A1713705994%2C%22kw%22%3A%22%E6%B5%99%E6%B1%9F%E5%A4%A9%E4%B8%8B%E5%95%86%E5%B8%AE%E7%A7%91%E6%8A%80%E8%82%A1%E4%BB%BD%E6%9C%89%E9%99%90%E5%85%AC%E5%8F%B8%208637%22%7D%2C%7B%22time%22%3A1714668512%2C%22kw%22%3A%22caswl%22%7D%2C%7B%22time%22%3A1715830502%2C%22kw%22%3A%22%5Beacces%5D%22%2C%22fq%22%3A2%7D%2C%7B%22time%22%3A1722177829%2C%22kw%22%3A%22ping%E6%B5%8B%E8%AF%95%22%2C%22fq%22%3A2%7D%2C%7B%22time%22%3A1729524662%2C%22kw%22%3A%22122.228.207.52%22%7D%2C%7B%22time%22%3A1730103268%2C%22kw%22%3A%22%E9%99%88%E8%8A%AF%E6%80%A1%E7%99%BE%E7%A7%91%22%7D%2C%7B%22time%22%3A1730103270%2C%22kw%22%3A%22%E9%99%88%E8%8A%AF%E6%80%A1%22%7D%2C%7B%22time%22%3A1734341253%2C%22kw%22%3A%22cadvisor%20%E7%9B%91%E6%8E%A7%22%7D%2C%7B%22time%22%3A1736225557%2C%22kw%22%3A%22curl%22%7D%5D&req=2&bs=curl&pbs=curl&csor=4&pwd=curl&sugmode=2&hot_launch=0&cb=jQuery1102010998424233041248_1736225551313&_=1736225551320' \\
                                              -H 'Accept: text/javascript, application/javascript, application/ecmascript, application/x-ecmascript, */*; q=0.01' \\
                                              -H 'Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6' \\
                                              -H 'Cache-Control: no-cache' \\
                                              -H 'Connection: keep-alive' \\
                                              -H 'Cookie: BIDUPSID=B5E9D9B5A8D4D4E64F990E0796449678; PSTM=1717744132; BAIDUID=976B1D46CB24FBB12E76E034A820CAE8:FG=1; BAIDUID_BFESS=976B1D46CB24FBB12E76E034A820CAE8:FG=1; BDUSS=RSVWpmVHhlQ25kYWVlM3hEekd1NmFVZXUzVkptOXlvQi9pUzF1VWRCeWs5dlJtQUFBQUFBPT0AAAAAAAAAAO2rMEa~bvwCcXR4eG0AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAKRpzWafTHMFRk; BDUSS_BFESS=RSVWpmVHhlQ25kYWVlM3hEekd1NmFVZXUzVkptOXlvQi9pUzF1VWRCeWs5dlJtQUFBQUFBPT0AAAAAAAAAAO2rMEa~bvwCcXR4eG0AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAKRpzWafTHMFRk; H_WISE_SIDS=110085_603322_307086_619664_621397_610631_621491_1992049_620487_623536_607027_625159_625168_621954_623990_625577_623878_623875_625441_625783_625964_625970_625624_626068_626128_626070_1991790_626544_626434_626375_626722_624023_626782_626773_626881_626986_626929_627080_627137_626980_627223_627238_625017_627263_627213_627286_624161_625250_627455_624517_625313_627379_627378_627592_627513_620143_627486_627708_627719_627789_627702_627498_627492_624663_627925_614026_612952_628177_628207_628198_628208_625432_628284_627934_628155_627899_628356_628307_628315_628298_628241_628426_622875_628534_628539_628546_628542_627870_628507_623211_628598_628296_628558_628762_628774_623076_628780_628783_628784_628832_628764_628854; MSA_WH=1705_1170; MCITY=-180%3A; delPer=0; BD_CK_SAM=1; PSINO=5; BDRCVFR[k-3xBxsWSJs]=mk3SLVN4HKm; BD_UPN=123253; ZFY=9PqLM6rdLzXbH6oTraFDALjf79q0DQ5i832hx7TWKXE:C; H_PS_PSSID=61027_61216_61361_60853_61530_61534_61608_61721_61729; BA_HECTOR=aka0000l0ha18h0g2l8ka1a0am14371jnpcog1v; H_PS_645EC=991dibuEh7UqInS4t%2FVsHSCs3hFmHO6OVe9UVV0jMGrS5ArS49uK%2FafVZvOtD7kYsWvyiV4; H_WISE_SIDS_BFESS=110085_603322_307086_619664_621397_610631_621491_1992049_620487_623536_607027_625159_625168_621954_623990_625577_623878_623875_625441_625783_625964_625970_625624_626068_626128_626070_1991790_626544_626434_626375_626722_624023_626782_626773_626881_626986_626929_627080_627137_626980_627223_627238_625017_627263_627213_627286_624161_625250_627455_624517_625313_627379_627378_627592_627513_620143_627486_627708_627719_627789_627702_627498_627492_624663_627925_614026_612952_628177_628207_628198_628208_625432_628284_627934_628155_627899_628356_628307_628315_628298_628241_628426_622875_628534_628539_628546_628542_627870_628507_623211_628598_628296_628558_628762_628774_623076_628780_628783_628784_628832_628764_628854; BDSVRTM=265; BDORZ=FFFB88E999055A3F8A630C64834BD6D0; WWW_ST=1736225563523' \\
                                              -H 'DNT: 1' \\
                                              -H 'Pragma: no-cache' \\
                                              -H 'Ps-Dataurlconfigqid: 0xb325e4c1001cfc0e' \\
                                              -H 'Referer: https://www.baidu.com/s?wd=curl&rsv_spt=1&rsv_iqid=0xb325e4c1001cfc0e&issp=1&f=8&rsv_bp=1&rsv_idx=2&ie=utf-8&rqlang=cn&tn=68018901_16_pg&rsv_enter=0&rsv_dl=tb&oq=curl&rsv_btype=t&rsv_t=991dibuEh7UqInS4t%2FVsHSCs3hFmHO6OVe9UVV0jMGrS5ArS49uK%2FafVZvOtD7kYsWvyiV4&rsv_pq=96b67c80001ed1f2' \\
                                              -H 'Sec-Fetch-Dest: empty' \\
                                              -H 'Sec-Fetch-Mode: cors' \\
                                              -H 'Sec-Fetch-Site: same-origin' \\
                                              -H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0' \\
                                              -H 'X-Requested-With: XMLHttpRequest' \\
                                              -H 'sec-ch-ua: "Microsoft Edge";v="131", "Chromium";v="131", "Not_A Brand";v="24"' \\
                                              -H 'sec-ch-ua-mobile: ?0' \\
                                              -H 'sec-ch-ua-platform: "macOS"'
                """;
        HttpUriRequest request = HttpClientUtils.curlToHttpRequest(curlCommand);
        assertEquals(
            "https://www.baidu.com/sugrec?pre=1&p=3&ie=utf-8&json=1&prod=pc&from=pc_web&sugsid=61027,61216,61361,60853,61530,61534,61608,61721,61729&wd=curl&his=%5B%7B%22time%22%3A1713705986%2C%22kw%22%3A%22%E5%A4%A9%E6%B5%99%E5%A4%A9%E4%B8%8B%E5%95%86%E5%B8%AE%E7%A7%91%E6%8A%80%E8%82%A1%E4%BB%BD%E6%9C%89%E9%99%90%E5%85%AC%E5%8F%B8%208637%22%7D%2C%7B%22time%22%3A1713705994%2C%22kw%22%3A%22%E6%B5%99%E6%B1%9F%E5%A4%A9%E4%B8%8B%E5%95%86%E5%B8%AE%E7%A7%91%E6%8A%80%E8%82%A1%E4%BB%BD%E6%9C%89%E9%99%90%E5%85%AC%E5%8F%B8%208637%22%7D%2C%7B%22time%22%3A1714668512%2C%22kw%22%3A%22caswl%22%7D%2C%7B%22time%22%3A1715830502%2C%22kw%22%3A%22%5Beacces%5D%22%2C%22fq%22%3A2%7D%2C%7B%22time%22%3A1722177829%2C%22kw%22%3A%22ping%E6%B5%8B%E8%AF%95%22%2C%22fq%22%3A2%7D%2C%7B%22time%22%3A1729524662%2C%22kw%22%3A%22122.228.207.52%22%7D%2C%7B%22time%22%3A1730103268%2C%22kw%22%3A%22%E9%99%88%E8%8A%AF%E6%80%A1%E7%99%BE%E7%A7%91%22%7D%2C%7B%22time%22%3A1730103270%2C%22kw%22%3A%22%E9%99%88%E8%8A%AF%E6%80%A1%22%7D%2C%7B%22time%22%3A1734341253%2C%22kw%22%3A%22cadvisor%20%E7%9B%91%E6%8E%A7%22%7D%2C%7B%22time%22%3A1736225557%2C%22kw%22%3A%22curl%22%7D%5D&req=2&bs=curl&pbs=curl&csor=4&pwd=curl&sugmode=2&hot_launch=0&cb=jQuery1102010998424233041248_1736225551313&_=1736225551320",
            request.getUri().toString());
        assertEquals("GET", ((HttpUriRequestBase)request).getMethod());
        HttpResponse response = httpClient.execute(request);
        assertEquals(200, response.getCode());
        assertEquals("text/plain; charset=UTF-8", response.getHeader("Content-Type").getValue());
    }
}