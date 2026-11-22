export function homePathFor(role) {
  switch (role) {
    case "BUYER":
      return "/buyer";
    case "SELLER":
      return "/seller";
    case "AGENT":
      return "/agent";
    default:
      return "/login";
  }
}

export function roleLabel(role) {
  return role ? role.charAt(0) + role.slice(1).toLowerCase() : "";
}