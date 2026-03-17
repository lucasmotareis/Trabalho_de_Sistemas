import { createTheme } from "@mui/material/styles";

export const appTheme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#145DA0",
    },
    secondary: {
      main: "#2E8BC0",
    },
    background: {
      default: "#f3f6fa",
      paper: "#ffffff",
    },
  },
  shape: {
    borderRadius: 12,
  },
  typography: {
    fontFamily: '"Segoe UI", Arial, Helvetica, sans-serif',
  },
});
