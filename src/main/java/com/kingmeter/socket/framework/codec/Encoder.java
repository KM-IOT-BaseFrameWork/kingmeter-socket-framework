package com.kingmeter.socket.framework.codec;

import com.alibaba.fastjson.JSONObject;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.socket.framework.config.LoggerConfig;
import com.kingmeter.socket.framework.dto.ResponseBody;
import com.kingmeter.utils.ByteUtil;
import com.kingmeter.utils.CRCUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Encoder extends MessageToByteEncoder<ResponseBody> {


    private final LoggerConfig loggerConfig;

    public Encoder(LoggerConfig loggerConfig) {
        this.loggerConfig = loggerConfig;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseBody response, ByteBuf out) throws Exception {
        out.writeBytes(getResponseFinalByteArray(response, ctx));
//        ctx.flush();
    }


    private byte[] getResponseFinalByteArray(ResponseBody response, ChannelHandlerContext ctx) {
        if (loggerConfig.isLog_model()) {
            log.info(new KingMeterMarker("Socket,TCP_IO,3002"),
                    "{}|{}|{}|{}", response.getDeviceId(),
                    ByteUtil.bytesToHexString(response.getFunctionCodeArray()),
                    ctx.channel().id().asLongText(),
                    JSONObject.toJSONString(response));
        }

        //change data into hex string
        byte[] dataArray = response.getData().getBytes();

        int dataCountLength = response.getToken_length() + 3 + dataArray.length;

        byte[] result = new byte[dataCountLength + 8];
        byte[] checkByteArray = new byte[dataCountLength];

        result[0] = response.getSTART_CODE_1();
        result[1] = response.getSTART_CODE_2();

        result[2] = (byte) (dataCountLength / 256);
        result[3] = (byte) (dataCountLength % 256);

        System.arraycopy(response.getTokenArray(), 0, checkByteArray,
                0, response.getToken_length());

        checkByteArray[response.getToken_length()] = 0;

        System.arraycopy(response.getFunctionCodeArray(), 0, checkByteArray,
                response.getToken_length() + 1, response.getFunctionCodeArray().length);

        //6, data
        System.arraycopy(dataArray, 0, checkByteArray,
                response.getToken_length() + 1 + response.getFunctionCodeArray().length,
                dataArray.length);

        byte[] checkTmp = CRCUtils.getInstance().getCheckCrcArray(checkByteArray);

        System.arraycopy(checkByteArray, 0, result, 4, checkByteArray.length);

        result[checkByteArray.length + 4] = checkTmp[0];
        result[checkByteArray.length + 5] = checkTmp[1];

        result[checkByteArray.length + 6] = response.getEND_CODE_1();
        result[checkByteArray.length + 7] = response.getEND_CODE_2();


        if (loggerConfig.isLog_message()) {
            log.info(new KingMeterMarker("Socket,TCP_IO,2002"),
                    "{}|{}|{}|{}", response.getDeviceId(),
                    ByteUtil.bytesToHexString(response.getFunctionCodeArray()),
                    ctx.channel().id().asLongText(),
                    ByteUtil.bytesToHexString(result));
        }
        return result;
    }

}
