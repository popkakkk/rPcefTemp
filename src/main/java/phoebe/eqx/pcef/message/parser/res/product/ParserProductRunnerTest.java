package phoebe.eqx.pcef.message.parser.res.product;

import com.google.gson.Gson;

public class ParserProductRunnerTest {
    public static void main(String[] args) {

        String str = "{\n" +
                "   \"resultCode\":\"20000\",\n" +
                "   \"developerMessage\":\"Query product success\",\n" +
                "   \"userMessage\":\"Transaction success.\",\n" +
                "   \"moreInfo\":\"\",\n" +
                "   \"resultData\":{\n" +
                "      \"products\":[\n" +
                "         {\n" +
                "            \"_id\":\"5caf0d8edd70922b00cc4d4e\",\n" +
                "            \"productId\":\"PD-20148\",\n" +
                "            \"productCategory\":{\n" +
                "               \"categoryId\":\"5a829fec874d4f770a093414\",\n" +
                "               \"categoryName\":\"IoT\",\n" +
                "               \"categoryOrder\":1\n" +
                "            },\n" +
                "            \"productSubCategory\":{\n" +
                "               \"subCategoryId\":\"5a829fec874d4f770a093414\",\n" +
                "               \"subCategoryName\":\"IOT Solution\",\n" +
                "               \"subCategoryOrder\":1\n" +
                "            },\n" +
                "            \"status\":\"Active\",\n" +
                "            \"productPublic\":true,\n" +
                "            \"productName\":\"Amazon Echo\",\n" +
                "            \"productShortDesc\":\"Echo (1st Generation) - Smart speaker with Alexa - Charcoal Fabric\",\n" +
                "            \"productPriceDetail\":{\n" +
                "               \"priceMethod\":\"Price Plan\",\n" +
                "               \"conditionPricing\":[\n" +
                "                  {\n" +
                "                     \"priceCode\":\"PC-00001\",\n" +
                "                     \"name\":\"Plan A - Flat\",\n" +
                "                     \"conditionType\":\"amount\",\n" +
                "                     \"conditionDetail\":{\n" +
                "                        \"amount\":{\n" +
                "                           \"data\":1,\n" +
                "                           \"unit\":\"Piece\"\n" +
                "                        },\n" +
                "                        \"price\":{\n" +
                "                           \"data\":6499,\n" +
                "                           \"unit\":\"THB\"\n" +
                "                        },\n" +
                "                        \"pricingMethod\":\"flat\"\n" +
                "                     }\n" +
                "                  },\n" +
                "                  {\n" +
                "                     \"priceCode\":\"PC-00002\",\n" +
                "                     \"name\":\"Plan B - Tier\",\n" +
                "                     \"conditionType\":\"amount\",\n" +
                "                     \"conditionDetail\":{\n" +
                "                        \"amount\":{\n" +
                "                           \"data\":1,\n" +
                "                           \"unit\":\"Pieces\"\n" +
                "                        },\n" +
                "                        \"price\":{\n" +
                "                           \"data\":6499,\n" +
                "                           \"unit\":\"THB\"\n" +
                "                        },\n" +
                "                        \"pricingMethod\":\"tier\",\n" +
                "                        \"pricing\":[\n" +
                "                           {\n" +
                "                              \"from\":1,\n" +
                "                              \"to\":1,\n" +
                "                              \"price\":6499\n" +
                "                           },\n" +
                "                           {\n" +
                "                              \"from\":2,\n" +
                "                              \"to\":100,\n" +
                "                              \"price\":6399\n" +
                "                           },\n" +
                "                           {\n" +
                "                              \"from\":101,\n" +
                "                              \"to\":300,\n" +
                "                              \"price\":6299\n" +
                "                           },\n" +
                "                           {\n" +
                "                              \"from\":301,\n" +
                "                              \"to\":500,\n" +
                "                              \"price\":6199\n" +
                "                           },\n" +
                "                           {\n" +
                "                              \"from\":501,\n" +
                "                              \"to\":-1,\n" +
                "                              \"price\":5899\n" +
                "                           }\n" +
                "                        ]\n" +
                "                     }\n" +
                "                  },\n" +
                "                  {\n" +
                "                     \"priceCode\":\"PC-00003\",\n" +
                "                     \"name\":\"Plan C - Volumn\",\n" +
                "                     \"conditionType\":\"amount\",\n" +
                "                     \"conditionDetail\":{\n" +
                "                        \"amount\":{\n" +
                "                           \"data\":1,\n" +
                "                           \"unit\":\"Pieces\"\n" +
                "                        },\n" +
                "                        \"price\":{\n" +
                "                           \"data\":6499,\n" +
                "                           \"unit\":\"THB\"\n" +
                "                        },\n" +
                "                        \"pricingMethod\":\"volumn\",\n" +
                "                        \"pricing\":[\n" +
                "                           {\n" +
                "                              \"from\":1,\n" +
                "                              \"to\":1,\n" +
                "                              \"price\":6499\n" +
                "                           },\n" +
                "                           {\n" +
                "                              \"from\":2,\n" +
                "                              \"to\":100,\n" +
                "                              \"price\":6450\n" +
                "                           },\n" +
                "                           {\n" +
                "                              \"from\":101,\n" +
                "                              \"to\":300,\n" +
                "                              \"price\":6250\n" +
                "                           },\n" +
                "                           {\n" +
                "                              \"from\":301,\n" +
                "                              \"to\":500,\n" +
                "                              \"price\":6199\n" +
                "                           },\n" +
                "                           {\n" +
                "                              \"from\":501,\n" +
                "                              \"to\":-1,\n" +
                "                              \"price\":6000\n" +
                "                           }\n" +
                "                        ]\n" +
                "                     }\n" +
                "                  }\n" +
                "               ]\n" +
                "            },\n" +
                "            \"productImage\":[\n" +
                "               {\n" +
                "                  \"type\":\"thumbnail\",\n" +
                "                  \"imagePath\":\"https://assets.pcmag.com/media/images/522880-amazon-echo-plus.jpg\"\n" +
                "               },\n" +
                "               {\n" +
                "                  \"type\":\"gallery\",\n" +
                "                  \"imagePath\":\"https://assets.pcmag.com/media/images/522880-amazon-echo-plus.jpg\"\n" +
                "               },\n" +
                "               {\n" +
                "                  \"type\":\"gallery\",\n" +
                "                  \"imagePath\":\"https://brain-images-ssl.cdn.dixons.com/4/8/10171184/l_10171184_003.jpg\"\n" +
                "               },\n" +
                "               {\n" +
                "                  \"type\":\"gallery\",\n" +
                "                  \"imagePath\":\"https://brain-images-ssl.cdn.dixons.com/4/8/10171184/l_10171184_006.jpg\"\n" +
                "               },\n" +
                "               {\n" +
                "                  \"type\":\"gallery\",\n" +
                "                  \"imagePath\":\"https://brain-images-ssl.cdn.dixons.com/4/8/10171184/l_10171184_007.jpg\"\n" +
                "               }\n" +
                "            ]\n" +
                "         }\n" +
                "      ],\n" +
                "      \"productTotal\":2\n" +
                "   }\n" +
                "}";
       GetResourceIdResponse  getResourceIdResponse = new Gson().fromJson(str, GetResourceIdResponse.class);
    }







}

