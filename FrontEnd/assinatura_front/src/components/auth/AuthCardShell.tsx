"use client";

import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

interface AuthCardShellProps {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}

export default function AuthCardShell({
  title,
  subtitle,
  children,
}: AuthCardShellProps) {
  return (
    <Stack
      direction="column"
      justifyContent="center"
      alignItems="center"
      sx={{
        minHeight: "100dvh",
        p: { xs: 2, sm: 4 },
        backgroundImage:
          "radial-gradient(circle at top, rgba(20,93,160,0.12), rgba(243,246,250,1) 50%)",
      }}
    >
      <Card
        variant="outlined"
        sx={{
          width: "100%",
          maxWidth: 480,
          p: { xs: 3, sm: 4 },
          borderRadius: 3,
          boxShadow:
            "0px 8px 24px rgba(16, 24, 40, 0.08), 0px 2px 6px rgba(16, 24, 40, 0.06)",
        }}
      >
        <Box sx={{ mb: 3 }}>
          <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
            {title}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        </Box>
        {children}
      </Card>
    </Stack>
  );
}
