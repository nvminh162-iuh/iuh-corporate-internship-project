import userService from "@/services/user.service";

const storageService = {
  async uploadUserAvatar(file: File): Promise<string> {
    const result = await userService.updateAvatar(file);
    return result.avatarUrl ?? "";
  },
};

export default storageService;
