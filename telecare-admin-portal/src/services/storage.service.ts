import userService from "@/services/user.service";

const storageService = {
  async uploadUserAvatar(file: File, _userId?: string): Promise<string> {
    const result = await userService.updateAvatar(file);
    return result.avatarUrl ?? "";
  },
};

export default storageService;
