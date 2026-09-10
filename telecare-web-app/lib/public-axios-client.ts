import axios, { type AxiosInstance } from "axios";

const publicAxiosClient: AxiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_GATEWAY_BASE_URL || "http://localhost:58080",
});

export default publicAxiosClient;
